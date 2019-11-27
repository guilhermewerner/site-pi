using System;
using System.Collections.Generic;
using System.ComponentModel.DataAnnotations;
using System.ComponentModel.DataAnnotations.Schema;
using System.Linq;
using System.Threading.Tasks;
using Microsoft.EntityFrameworkCore;
using SitePi.Models;

namespace SitePi.Models
{
    public class Choice
    {
        [Key]
        public int Id { get; set; }

        [Required]
        public string Text { get; set; }

        public int QuestionId { get; set; }
        public Question Question { get; set; }
        
        public int ProfileId { get; set; }
        public Profile Profile { get; set; }
    }
}
